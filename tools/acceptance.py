#!/usr/bin/env python3
"""Black-box acceptance harness for the e-commerce POC.

Tests ANY backend implementing CONTRACT.md over HTTP. Standard library only,
so it runs anywhere (VM, Mac, CI) with no dependencies.

Usage:
    python3 tools/acceptance.py --base http://localhost:8000 --seed seed

Exit code 0 = all checks passed, 1 = at least one failed.
Designed for a freshly-seeded environment. It places orders and creates one
test product (unique SKU per run), so repeated runs accumulate test data —
re-seed for a pristine state.
"""
import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request
from datetime import date, timedelta


class Fail(Exception):
    pass


def aeq(a, b, msg):
    if a != b:
        raise Fail(f"{msg}: expected {b!r}, got {a!r}")


def money_eq(a, b, msg):
    if abs(float(a) - float(b)) > 0.01:
        raise Fail(f"{msg}: expected {b}, got {a}")


class Harness:
    def __init__(self, base, seed_dir):
        self.base = base.rstrip("/")
        self.seed_dir = seed_dir
        self.results = []
        self.ctx = {}

    # -- http -----------------------------------------------------------
    def req(self, method, path, body=None):
        data = json.dumps(body).encode() if body is not None else None
        r = urllib.request.Request(
            self.base + path, data=data, method=method,
            headers={"Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(r, timeout=15) as resp:
                raw = resp.read().decode() or "null"
                return resp.status, json.loads(raw)
        except urllib.error.HTTPError as e:
            try:
                payload = json.loads(e.read().decode() or "null")
            except Exception:
                payload = None
            return e.code, payload
        except Exception as e:
            raise Fail(f"connection failed for {method} {path}: {e}")

    def check(self, name, fn):
        try:
            fn()
            self.results.append((name, True, ""))
            print(f"  PASS  {name}", flush=True)
        except Fail as e:
            self.results.append((name, False, str(e)))
            print(f"  FAIL  {name}: {e}", flush=True)
        except Exception as e:
            self.results.append((name, False, f"unexpected: {type(e).__name__}: {e}"))
            print(f"  ERROR {name}: {type(e).__name__}: {e}", flush=True)

    def need(self, key):
        if key not in self.ctx:
            raise Fail(f"skipped: prerequisite check failed (no '{key}')")
        return self.ctx[key]

    # -- checks ----------------------------------------------------------
    def t_products_list(self):
        st, body = self.req("GET", "/api/products")
        aeq(st, 200, "GET /api/products status")
        aeq(body["total"], 25, "active product count")
        prods = body["products"]
        aeq(len(prods), 25, "products array length")
        required = {"id", "sku", "title", "description", "price", "category",
                    "tags", "attributes", "variants", "active"}
        for p in prods:
            missing = required - set(p.keys())
            if missing:
                raise Fail(f"product {p.get('sku')}: missing fields {missing}")
            if p["active"] is not True:
                raise Fail(f"product {p['sku']}: inactive product listed")
            if not p["variants"]:
                raise Fail(f"product {p['sku']}: empty variants")
        titles = [p["title"] for p in prods]
        if "Old Gadget Pro" in titles:
            raise Fail("inactive product 'Old Gadget Pro' visible on storefront")
        self.ctx["products"] = prods

    def t_product_detail(self):
        pid = self.need("products")[0]["id"]
        st, body = self.req("GET", f"/api/products/{pid}")
        aeq(st, 200, "GET /api/products/:id status")
        aeq(body["id"], pid, "product id round-trip")
        st, _ = self.req("GET", "/api/products/000000000000000000000000")
        aeq(st, 404, "GET /api/products/:bogus status")

    def t_product_create_update(self):
        sku = f"HARNESS-{int(time.time())}"
        payload = {"sku": sku, "title": "Harness Test Product",
                   "description": "created by acceptance harness",
                   "price": 9.99, "category": "cables", "tags": ["test"],
                   "attributes": {"color": "red"},
                   "variants": [{"sku": sku + "-V", "color": "red", "stock": 5}],
                   "active": True}
        st, body = self.req("POST", "/api/products", payload)
        aeq(st, 201, "POST /api/products status")
        pid = body["id"]
        st, body = self.req("PUT", f"/api/products/{pid}", {"price": 19.99})
        aeq(st, 200, "PUT /api/products/:id status")
        money_eq(body["price"], 19.99, "updated price")
        st, body = self.req("GET", f"/api/products/{pid}")
        money_eq(body["price"], 19.99, "price after re-read")

    def t_order_place(self):
        prods = self.need("products")
        p0, p1 = prods[0], prods[1]
        st, body = self.req("POST", "/api/orders",
                            {"user_id": 1,
                             "items": [{"product_id": p0["id"], "quantity": 2},
                                       {"product_id": p1["id"], "quantity": 1}]})
        aeq(st, 201, "POST /api/orders status")
        expected = round(p0["price"] * 2 + p1["price"] * 1, 2)
        money_eq(body["total_amount"], expected, "order total")
        aeq(body["status"], "PENDING", "new order status")
        aeq(body["user_id"], 1, "order user")
        snap = {i["product_id"]: i for i in body["items"]}
        for p, q in ((p0, 2), (p1, 1)):
            s = snap[p["id"]]
            aeq(s["title"], p["title"], "snapshot title")
            money_eq(s["unit_price"], p["price"], "snapshot price")
            aeq(s["quantity"], q, "quantity")
        self.ctx["order_id"] = body["id"]

    def t_order_invalid_rejected(self):
        st, body = self.req("POST", "/api/search/orders", {})
        aeq(st, 200, "search status for baseline")
        before = body["total"]
        st, _ = self.req("POST", "/api/orders",
                         {"user_id": 1,
                          "items": [{"product_id": "000000000000000000000000",
                                     "quantity": 1}]})
        if st != 400:
            raise Fail(f"bogus product_id: expected 400, got {st}")
        inactive_id = None
        with open(f"{self.seed_dir}/products.json") as f:
            for p in json.load(f):
                if p["active"] is False:
                    inactive_id = p["_id"]
        st, _ = self.req("POST", "/api/orders",
                         {"user_id": 1,
                          "items": [{"product_id": inactive_id, "quantity": 1}]})
        if st != 400:
            raise Fail(f"inactive product: expected 400, got {st}")
        st, body = self.req("POST", "/api/search/orders", {})
        aeq(body["total"], before, "order count after rejected placements")

    def t_order_detail(self):
        oid_ = self.need("order_id")
        st, body = self.req("GET", f"/api/orders/{oid_}")
        aeq(st, 200, "GET /api/orders/:id status")
        aeq(body["id"], oid_, "order id round-trip")
        aeq(body["customer"]["name"], "Aarav Sharma", "customer name")
        st, _ = self.req("GET", "/api/orders/999999")
        aeq(st, 404, "GET /api/orders/:bogus status")

    def t_status_update(self):
        oid_ = self.need("order_id")
        st, body = self.req("PATCH", f"/api/orders/{oid_}/status",
                            {"status": "SHIPPED"})
        aeq(st, 200, "PATCH status status")
        aeq(body["status"], "SHIPPED", "updated status")
        st, body = self.req("GET", f"/api/orders/{oid_}")
        aeq(body["status"], "SHIPPED", "status after re-read")
        st, _ = self.req("PATCH", f"/api/orders/{oid_}/status",
                         {"status": "BOGUS"})
        if st not in (400, 422):
            raise Fail(f"invalid status value: expected 400/422, got {st}")

    def t_search_omni(self):
        st, body = self.req("POST", "/api/search/orders",
                            {"query": "wireless", "page_size": 100})
        aeq(st, 200, "search status")
        if body["total"] < 8:
            raise Fail(f"omni 'wireless': total {body['total']}, expected >= 8")
        st, body = self.req("POST", "/api/search/orders",
                            {"query": "kabir", "page_size": 100})
        aeq(st, 200, "customer-name search status")
        names = [o["customer"]["name"] for o in body["orders"]]
        if "Kabir Malhotra" not in names:
            raise Fail("omni 'kabir' missed customer 'Kabir Malhotra'")

    def t_search_facets(self):
        st, body = self.req("POST", "/api/search/orders",
                            {"statuses": ["SHIPPED"], "page_size": 100})
        aeq(st, 200, "status facet status")
        if body["total"] < 1:
            raise Fail("status facet SHIPPED returned nothing")
        for o in body["orders"]:
            aeq(o["status"], "SHIPPED", "facet status mismatch")
        st, body = self.req("POST", "/api/search/orders",
                            {"min_total": 10000, "page_size": 100})
        aeq(st, 200, "price facet status")
        if body["total"] < 5:
            raise Fail(f"price facet >=200: total {body['total']}, expected >= 5")
        for o in body["orders"]:
            if float(o["total_amount"]) < 200 - 0.01:
                raise Fail(f"price facet leaked order {o['order_id']}")
        today = date.today().isoformat()
        week_ago = (date.today() - timedelta(days=7)).isoformat()
        st, body = self.req("POST", "/api/search/orders",
                            {"date_from": week_ago, "date_to": today,
                             "page_size": 100})
        aeq(st, 200, "date facet status")
        if body["total"] < 5:
            raise Fail(f"date facet last 7d: total {body['total']}, expected >= 5")

    def t_search_kpis(self):
        st, body = self.req("POST", "/api/search/orders",
                            {"page": 1, "page_size": 1000})
        aeq(st, 200, "kpi search status")
        kpis = body["kpis"]
        if sum(kpis["count_by_status"].values()) != body["total"]:
            raise Fail("count_by_status does not sum to total")
        revenue = sum(float(o["total_amount"]) for o in body["orders"])
        if abs(revenue - float(kpis["total_revenue"])) > 1.0:
            raise Fail(f"total_revenue {kpis['total_revenue']} != sum {revenue}")

    def t_sync_new_order(self):
        st, body = self.req("POST", "/api/search/orders",
                            {"statuses": ["PENDING"]})
        before = body["total"]
        p = self.need("products")[5]
        st, placed = self.req("POST", "/api/orders",
                              {"user_id": 2,
                               "items": [{"product_id": p["id"], "quantity": 1}]})
        aeq(st, 201, "sync probe order placement")
        deadline = time.time() + 90
        while time.time() < deadline:
            st, body = self.req("POST", "/api/search/orders",
                                {"statuses": ["PENDING"]})
            if body["total"] == before + 1:
                return
            time.sleep(3)
        raise Fail("new order not visible in search after 90s (sync broken?)")

    def t_snapshot_intact(self):
        st, body = self.req("GET", "/api/products")
        wm = next(p for p in body["products"] if p["sku"] == "WM-001")
        aeq(wm["title"], "Wireless Mouse Pro", "catalog title after rename")
        money_eq(wm["price"], 4999, "catalog price after rename")
        st, body = self.req("GET", "/api/orders/1")
        aeq(st, 200, "seeded order detail status")
        titles = [i["title"] for i in body["items"]]
        if "Wireless Mouse" not in titles:
            raise Fail(f"seeded order lost snapshot title: {titles}")

    # -- run -------------------------------------------------------------
    def run(self):
        print(f"acceptance harness -> {self.base}")
        order = ["t_products_list", "t_product_detail", "t_product_create_update",
                 "t_order_place", "t_order_invalid_rejected", "t_order_detail",
                 "t_status_update", "t_search_omni", "t_search_facets",
                 "t_search_kpis", "t_sync_new_order", "t_snapshot_intact"]
        for name in order:
            self.check(name, getattr(self, name))
        passed = sum(1 for _, ok, _ in self.results if ok)
        total = len(self.results)
        print(f"\n{passed}/{total} checks passed")
        return 0 if passed == total else 1


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://localhost:8000")
    ap.add_argument("--seed", default=None,
                    help="path to seed dir (default: ../seed relative to script)")
    args = ap.parse_args()
    seed = args.seed or os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                    "..", "seed")
    sys.exit(Harness(args.base, seed).run())


if __name__ == "__main__":
    main()
