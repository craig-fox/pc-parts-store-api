package nz.fox.craig.api.gateway;

public class Payloads {

    public static final String ORDER_REQUEST = """
    {
        "customerId": "550e8400-e29b-41d4-a716-446655440000",
        "items": [
            {
                "productId": "750e8400-e29b-41d4-a716-446655440000",
                "quantity": 1
            }
        ]
    }
    """;

    public static final String ORDER_RESPONSE = """
    {
        "id": "650e8400-e29b-41d4-a716-446655440000",
        "customerId": "550e8400-e29b-41d4-a716-446655440000",
        "orderDate": "2026-08-25T14:00:00",
        "status": "PLACED",
        "subtotal": 100.00,
        "shipping": 15.00,
        "total": 115.00,
        "items": [
            {
                "productId": "750e8400-e29b-41d4-a716-446655440000",
                "productName": "Test Product",
                "quantity": 1,
                "unitPrice": 100.00,
                "lineTotal": 100.00
            }
        ]
    }
    """;

    public static final String CUSTOMER_REQUEST = """
    {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "firstName": "John",
        "lastName": "Smith",
        "displayName": "John Smith",
        "email": "john.smith@example.com",
        "address": "123 Test Street",
        "status": "ACTIVE"
    }
    """;

    public static final String CUSTOMER_RESPONSE = """ 
    {
        "firstName": "John",
        "lastName": "Smith",
        "email": "john.smith@example.com",
        "address": "123 Test Street"
    }
    """;

    public static final String ORDERS_RETURNED = """
    [
        {
            "id": "650e8400-e29b-41d4-a716-446655440000",
            "customerId": "550e8400-e29b-41d4-a716-446655440000",
            "orderDate": "2026-08-25T14:00:00",
            "status": "PLACED",
            "subtotal": 100.00,
            "shipping": 15.00,
            "total": 115.00,
            "items": [
                {
                    "productId": "750e8400-e29b-41d4-a716-446655440000",
                    "productName": "Test Product",
                    "quantity": 1,
                    "unitPrice": 100.00,
                    "lineTotal": 100.00
                }
            ]
        }
    ]
    """;

}
