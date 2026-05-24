using WarehouseService.Infrastructure.Persistence.Entities;

namespace WarehouseService.Infrastructure.Persistence.Configurations;

internal static class WarehouseProductSeed
{
    public static readonly WarehouseProductEntity[] Products =
    [
        new() { ProductId = Guid.Parse("11111111-1111-4111-8111-111111111111"), Sku = "SKU-CHAIR-OAK", Name = "Esszimmerstuhl Eiche", Category = "Stuehle", AvailableQuantity = 24 },
        new() { ProductId = Guid.Parse("22222222-2222-4222-8222-222222222222"), Sku = "SKU-DESK-OSLO", Name = "Schreibtisch Oslo", Category = "Tische", AvailableQuantity = 12 },
        new() { ProductId = Guid.Parse("33333333-3333-4333-8333-333333333333"), Sku = "SKU-LAMP-LUNA", Name = "Stehlampe Luna", Category = "Lampen", AvailableQuantity = 30 },
        new() { ProductId = Guid.Parse("44444444-4444-4444-8444-444444444444"), Sku = "SKU-SOFA-NORDIC", Name = "Dreisitzer Sofa Nordic", Category = "Sofas", AvailableQuantity = 8 },
        new() { ProductId = Guid.Parse("55555555-5555-4555-8555-555555555555"), Sku = "SKU-BED-LINA", Name = "Doppelbett Lina 180x200", Category = "Betten", AvailableQuantity = 7 },
        new() { ProductId = Guid.Parse("66666666-6666-4666-8666-666666666666"), Sku = "SKU-WARDROBE-MALMO", Name = "Kleiderschrank Malmo", Category = "Schraenke", AvailableQuantity = 6 },
        new() { ProductId = Guid.Parse("77777777-7777-4777-8777-777777777777"), Sku = "SKU-SHELF-BERGEN", Name = "Buecherregal Bergen", Category = "Regale", AvailableQuantity = 18 },
        new() { ProductId = Guid.Parse("88888888-8888-4888-8888-888888888888"), Sku = "SKU-TABLE-ROUND", Name = "Runder Esstisch Mira", Category = "Tische", AvailableQuantity = 9 },
        new() { ProductId = Guid.Parse("99999999-9999-4999-8999-999999999999"), Sku = "SKU-COFFEE-TABLE", Name = "Couchtisch Fino", Category = "Tische", AvailableQuantity = 15 },
        new() { ProductId = Guid.Parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"), Sku = "SKU-TV-BOARD", Name = "TV-Board Bergen", Category = "Sideboards", AvailableQuantity = 11 },
        new() { ProductId = Guid.Parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"), Sku = "SKU-NIGHTSTAND", Name = "Nachttisch Liva", Category = "Schlafzimmer", AvailableQuantity = 20 },
        new() { ProductId = Guid.Parse("cccccccc-cccc-4ccc-8ccc-cccccccccccc"), Sku = "SKU-SIDEBOARD-NOVA", Name = "Sideboard Nova", Category = "Sideboards", AvailableQuantity = 10 },
        new() { ProductId = Guid.Parse("dddddddd-dddd-4ddd-8ddd-dddddddddddd"), Sku = "SKU-OFFICE-CHAIR", Name = "Buerostuhl Ergo", Category = "Stuehle", AvailableQuantity = 16 },
        new() { ProductId = Guid.Parse("eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee"), Sku = "SKU-DINING-BENCH", Name = "Essbank Kopenhagen", Category = "Sitzbaenke", AvailableQuantity = 10 },
        new() { ProductId = Guid.Parse("ffffffff-ffff-4fff-8fff-ffffffffffff"), Sku = "SKU-SHOE-CABINET", Name = "Schuhschrank Fynn", Category = "Flur", AvailableQuantity = 14 },
        new() { ProductId = Guid.Parse("12121212-1212-4212-8212-121212121212"), Sku = "SKU-DRESSER-ELLA", Name = "Kommode Ella", Category = "Schraenke", AvailableQuantity = 13 },
        new() { ProductId = Guid.Parse("34343434-3434-4434-8434-343434343434"), Sku = "SKU-ARMCHAIR-MIKA", Name = "Sessel Mika", Category = "Sessel", AvailableQuantity = 9 },
        new() { ProductId = Guid.Parse("56565656-5656-4565-8565-565656565656"), Sku = "SKU-BAR-STOOL", Name = "Barhocker Loft", Category = "Stuehle", AvailableQuantity = 22 },
        new() { ProductId = Guid.Parse("78787878-7878-4787-8787-787878787878"), Sku = "SKU-GARDEN-CHAIR", Name = "Gartenstuhl Palma", Category = "Garten", AvailableQuantity = 28 },
        new() { ProductId = Guid.Parse("90909090-9090-4909-8909-909090909090"), Sku = "SKU-GARDEN-TABLE", Name = "Gartentisch Palma", Category = "Garten", AvailableQuantity = 8 }
    ];
}
