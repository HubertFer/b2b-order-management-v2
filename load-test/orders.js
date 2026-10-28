import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const RATE = Number(__ENV.RATE || 100);
const DURATION = __ENV.DURATION || '1m';
const PARTNERS = 50;
const HOT_PARTNER_LIMIT = 1000;
const HOT_ORDER_PRICE = 10;

const JSON_HEADERS = { 'Content-Type': 'application/json' };
const hotApprovals = new Counter('hot_partner_approvals');
const hotRejections = new Counter('hot_partner_rejections');

export const options = {
    scenarios: {
        order_lifecycle: {
            executor: 'constant-arrival-rate',
            exec: 'orderLifecycle',
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: 100,
            maxVUs: 400,
        },
        search: {
            executor: 'constant-arrival-rate',
            exec: 'search',
            rate: Math.max(1, Math.round(RATE / 4)),
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: 10,
            maxVUs: 100,
        },
        hot_partner: {
            executor: 'shared-iterations',
            exec: 'hotPartner',
            vus: 50,
            iterations: 5 * HOT_PARTNER_LIMIT / HOT_ORDER_PRICE,
            maxDuration: DURATION,
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        'http_req_duration{scenario:order_lifecycle}': ['p(95)<500'],
        'http_req_duration{scenario:search}': ['p(95)<500'],
        'checks{check:credit limit respected}': ['rate==1'],
        'checks{check:hot partner credit fully used}': ['rate==1'],
        'checks{check:rejection shows available credit}': ['rate==1'],
    },
};

export function setup() {
    const partners = [];
    for (let i = 0; i < PARTNERS; i++) {
        partners.push(createPartner(1000000000));
    }
    return { partners, hotPartner: createPartner(HOT_PARTNER_LIMIT) };
}

export function orderLifecycle(data) {
    const partnerId = data.partners[Math.floor(Math.random() * data.partners.length)];
    const body = orderBody(partnerId, 1 + Math.floor(Math.random() * 5), 10 + Math.floor(Math.random() * 90));
    const key = `${__VU}-${__ITER}-${Date.now()}`;

    const created = http.post(`${BASE_URL}/orders`, body, {
        headers: { ...JSON_HEADERS, 'Idempotency-Key': key },
        tags: { name: 'POST /orders' },
    });
    if (!check(created, { 'order created': (r) => r.status === 201 })) {
        return;
    }
    const orderId = created.json('id');

    const retried = http.post(`${BASE_URL}/orders`, body, {
        headers: { ...JSON_HEADERS, 'Idempotency-Key': key },
        tags: { name: 'POST /orders (retry)' },
    });
    check(retried, { 'retry returns same order': (r) => r.status === 200 && r.json('id') === orderId });

    const approved = http.post(`${BASE_URL}/orders/${orderId}/approve`, null,
        { tags: { name: 'POST /orders/{id}/approve' } });
    check(approved, { 'order approved': (r) => r.status === 200 });

    const fetched = http.get(`${BASE_URL}/orders/${orderId}`, { tags: { name: 'GET /orders/{id}' } });
    check(fetched, { 'order is APROVADO': (r) => r.status === 200 && r.json('status') === 'APROVADO' });
}

export function search() {
    const status = Math.random() < 0.5 ? 'APROVADO' : 'PENDENTE';
    const response = http.get(`${BASE_URL}/orders?status=${status}&size=20`, { tags: { name: 'GET /orders' } });
    check(response, { 'search succeeded': (r) => r.status === 200 });
}

export function hotPartner(data) {
    const created = http.post(`${BASE_URL}/orders`, orderBody(data.hotPartner, 1, HOT_ORDER_PRICE), {
        headers: JSON_HEADERS,
        tags: { name: 'POST /orders (hot partner)' },
        responseCallback: http.expectedStatuses(201, 422),
    });
    if (created.status !== 201) {
        hotRejections.add(1);
        checkRejectionShowsCredit(created);
        return;
    }
    const approved = http.post(`${BASE_URL}/orders/${created.json('id')}/approve`, null, {
        tags: { name: 'POST /orders/{id}/approve (hot partner)' },
        responseCallback: http.expectedStatuses(200, 422),
    });
    if (approved.status === 200) {
        hotApprovals.add(1);
    } else {
        hotRejections.add(1);
        checkRejectionShowsCredit(approved);
    }
}

function checkRejectionShowsCredit(response) {
    check(response, {
        'rejection shows available credit': (r) =>
            r.json('availableCredit') < HOT_ORDER_PRICE && r.json('requestedAmount') === HOT_ORDER_PRICE,
    });
}

export function teardown(data) {
    const partner = http.get(`${BASE_URL}/partners/${data.hotPartner}`).json();
    check(partner, {
        'credit limit respected': (p) => p.usedCredit <= p.creditLimit && p.availableCredit >= 0,
        'hot partner credit fully used': (p) => p.usedCredit === p.creditLimit,
    });
    console.log(`Hot partner: limit=${partner.creditLimit} used=${partner.usedCredit} available=${partner.availableCredit}`);
}

function createPartner(creditLimit) {
    const response = http.post(`${BASE_URL}/partners`, JSON.stringify({ creditLimit }), { headers: JSON_HEADERS });
    if (response.status !== 201) {
        throw new Error(`Could not create partner: ${response.status} ${response.body}`);
    }
    return response.json('id');
}

function orderBody(partnerId, quantity, unitPrice) {
    return JSON.stringify({
        partnerId,
        items: [{ productId: '33333333-3333-3333-3333-333333333333', quantity, unitPrice }],
    });
}
