import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
    vus: 5,
    duration: "30s",
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export default function () {
    const res = http.get(`${BASE_URL}/api/media`);

    check(res, {
        "status é 200": (r) => r.status === 200,
        "responde rápido (<500ms)": (r) => r.timings.duration < 500,
    });

    sleep(1);
}
