public static class TestData
{
    public static readonly Product Chair = new(
        Guid.Parse("11111111-1111-4111-8111-111111111111"),
        "SKU-CHAIR-OAK",
        "Esszimmerstuhl Eiche",
        "Stuehle",
        129.99m);

    public static readonly Product Desk = new(
        Guid.Parse("22222222-2222-4222-8222-222222222222"),
        "SKU-DESK-OSLO",
        "Schreibtisch Oslo",
        "Schreibtische",
        499.00m);

    public static readonly Product Sofa = new(
        Guid.Parse("33333333-3333-4333-8333-333333333333"),
        "SKU-SOFA-LIMA",
        "Sofa Lima",
        "Sofas",
        899.00m);

    public static readonly Product SoldOut = new(
        Guid.Parse("66666666-6666-4666-8666-666666666666"),
        "SKU-SOLD-OUT-TEST",
        "Ausverkaufter Testartikel",
        "Test",
        49.99m);
}

public sealed record Product(Guid ProductId, string Sku, string Name, string Category, decimal UnitPrice);
