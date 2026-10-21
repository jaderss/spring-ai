insert into customer (id, full_name) values
    ('CUST-0001', 'Ada Example'),
    ('CUST-0002', 'Bruno Sample');

insert into account (number, customer_id, type, currency, balance, opened_at) values
    ('ACME-CHK-0001', 'CUST-0001', 'CHECKING', 'USD', 8912.26, '2024-03-01T10:00:00Z'),
    ('ACME-SAV-0001', 'CUST-0001', 'SAVINGS', 'USD', 10012.50, '2024-03-01T10:05:00Z'),
    ('ACME-CHK-0003', 'CUST-0001', 'CHECKING', 'EUR', 500.00, '2025-06-15T14:00:00Z'),
    ('ACME-CHK-0002', 'CUST-0002', 'CHECKING', 'USD', 708.00, '2025-01-20T11:30:00Z');

insert into account_transaction (id, account_number, posted_at, description, amount, currency) values
    ('442d060c-04d6-5009-86a2-b7916ef31558', 'ACME-CHK-0001', '2026-09-01T09:00:00Z', 'Opening balance', 1500.00, 'USD'),
    ('8b3dad34-3af5-5fbe-bd0d-a2199c55a934', 'ACME-CHK-0001', '2026-09-01T12:00:00Z', 'Example Corp Payroll', 3200.00, 'USD'),
    ('f00f0dcc-e862-5314-a6fd-530aa3849544', 'ACME-CHK-0001', '2026-09-03T08:15:00Z', 'Example Coffee Roasters', -4.75, 'USD'),
    ('a008eb23-a599-5d77-853f-ad3680c5b88b', 'ACME-CHK-0001', '2026-09-05T18:40:00Z', 'Sample Grocery Mart', -86.40, 'USD'),
    ('bfbb2a01-7f5c-55ab-ae26-5e78617225ee', 'ACME-CHK-0001', '2026-09-10T07:00:00Z', 'Placeholder Power & Light', -112.35, 'USD'),
    ('314eab09-0d98-5164-b905-3dfafcf2392f', 'ACME-CHK-0001', '2026-09-15T12:00:00Z', 'Example Corp Payroll', 3200.00, 'USD'),
    ('dd1358ce-1b66-5bd4-8ec0-f244c454337b', 'ACME-CHK-0001', '2026-09-18T21:05:00Z', 'Fictional Streaming Co.', -15.99, 'USD'),
    ('df05eff3-40ab-575e-916e-5143bd406764', 'ACME-CHK-0001', '2026-09-22T17:30:00Z', 'Sample Grocery Mart', -64.10, 'USD'),
    ('230aefc7-ae1e-56fd-8e86-cd60665a0db0', 'ACME-CHK-0001', '2026-09-28T09:00:00Z', 'Demo Rent Management', -1850.00, 'USD'),
    ('6c63a6cc-f2b0-5e69-87b1-34e2c607321c', 'ACME-CHK-0001', '2026-10-01T12:00:00Z', 'Example Corp Payroll', 3200.00, 'USD'),
    ('56e19454-2727-5190-89d1-e9ef20bb7c0b', 'ACME-CHK-0001', '2026-10-02T08:20:00Z', 'Example Coffee Roasters', -5.25, 'USD'),
    ('5ec282a8-7901-53de-aa93-8ece25b89440', 'ACME-CHK-0001', '2026-10-06T19:10:00Z', 'Mock Fuel Station', -48.90, 'USD'),
    ('77bf4d53-11af-541f-8870-b1acc6f63265', 'ACME-SAV-0001', '2026-09-01T09:00:00Z', 'Opening balance', 10000.00, 'USD'),
    ('5e10be3c-4c89-542f-85d4-072767b87e91', 'ACME-SAV-0001', '2026-09-30T23:00:00Z', 'Interest', 12.50, 'USD'),
    ('f2371544-bbc1-5726-ab23-1a85118a4793', 'ACME-CHK-0003', '2026-09-01T09:00:00Z', 'Opening balance', 500.00, 'EUR'),
    ('aa535010-a662-593f-b805-c3db265f4ae1', 'ACME-CHK-0002', '2026-09-01T09:00:00Z', 'Opening balance', 750.00, 'USD'),
    ('ec0b70f7-20f0-5325-9d38-4e84310681e4', 'ACME-CHK-0002', '2026-09-12T16:45:00Z', 'Sample Grocery Mart', -42.00, 'USD');
