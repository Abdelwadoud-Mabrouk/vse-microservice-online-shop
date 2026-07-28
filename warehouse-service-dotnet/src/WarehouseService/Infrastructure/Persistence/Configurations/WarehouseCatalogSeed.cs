using WarehouseService.Infrastructure.Persistence.Entities;

namespace WarehouseService.Infrastructure.Persistence.Configurations;

internal static class WarehouseCatalogSeed
{
    public static readonly WarehouseProductEntity[] Products =
    [
        Product("c0000000-0000-0000-0000-000000000001", "SOFA-SAMT-ANTH-3S", "Samt Sofa Anthrazit 3-Sitzer", "Sofas"),
        Product("c0000000-0000-0000-0000-000000000002", "LDR-SOFA-COGNAC-2S", "Leder Sofa Cognac 2-Sitzer", "Sofas"),
        Product("c0000000-0000-0000-0000-000000000003", "ECKSOFA-GRAU-MODERN", "Ecksofa Grau Modern", "Sofas"),
        Product("c0000000-0000-0000-0000-000000000004", "SOFA-BEIGE-LOUNGE", "Lounge Sofa Beige", "Sofas"),
        Product("c0000000-0000-0000-0000-000000000005", "SOFA-CHESTER-BRAUN", "Chesterfield Sofa Braun", "Sofas"),
        Product("c0000000-0000-0000-0000-000000000006", "SOFA-MODUL-SCHWARZ", "Modulares Sofa Schwarz", "Sofas"),
        Product("c0000000-0000-0000-0000-000000000007", "SOFA-SCANDI-WEISS", "Scandi Sofa Weiß", "Sofas"),
        Product("c0000000-0000-0000-0000-000000000008", "SOFA-GRUEN-VELVET", "Velvet Sofa Grün", "Sofas"),
        Product("c0000000-0000-0000-0000-000000000009", "SOFA-SCHLAF-BLAU", "Sofa Bett Blau", "Sofas"),
        Product("c0000000-0000-0000-0000-000000000010", "SOFA-LUXUS-CREME", "Luxus Sofa Creme", "Sofas"),
        Product("c0000000-0000-0000-0000-000000000011", "TISCH-MARMOR-SCHWARZ", "Couchtisch Marmor Schwarz", "Couchtische"),
        Product("c0000000-0000-0000-0000-000000000012", "TISCH-EICHE-NATUR", "Couchtisch Eiche Natur", "Couchtische"),
        Product("c0000000-0000-0000-0000-000000000013", "TISCH-GLAS-MODERN", "Glas Couchtisch Modern", "Couchtische"),
        Product("c0000000-0000-0000-0000-000000000014", "TISCH-INDUSTRIAL-METALL", "Industrial Couchtisch Metall", "Couchtische"),
        Product("c0000000-0000-0000-0000-000000000015", "TISCH-RUND-HOLZ", "Runder Couchtisch Holz", "Couchtische"),
        Product("c0000000-0000-0000-0000-000000000016", "TV-MOEBEL-EICHE-180", "TV-Möbel Eiche Natur 180cm", "TV-Möbel"),
        Product("c0000000-0000-0000-0000-000000000017", "TV-LOWBOARD-SCHWARZ", "TV Lowboard Schwarz Modern", "TV-Möbel"),
        Product("c0000000-0000-0000-0000-000000000018", "TV-WAND-MODERN", "TV Wandkombination Modern", "TV-Möbel"),
        Product("c0000000-0000-0000-0000-000000000019", "TV-BOARD-INDUSTRIAL", "TV Board Industrial Style", "TV-Möbel"),
        Product("c0000000-0000-0000-0000-000000000020", "TV-MOEBEL-WEISS", "TV Möbel Weiß Hochglanz", "TV-Möbel"),
        Product("c0000000-0000-0000-0000-000000000021", "REGAL-EICHE-180", "Regal Eiche Natur 180cm", "Regale"),
        Product("c0000000-0000-0000-0000-000000000022", "REGAL-METALL-SCHWARZ", "Metall Regal Schwarz", "Regale"),
        Product("c0000000-0000-0000-0000-000000000023", "REGAL-WAND-SYSTEM", "Wandregal System Modern", "Regale"),
        Product("c0000000-0000-0000-0000-000000000024", "REGAL-LOFT-HOLZ", "Loft Regal Holz Metall", "Regale"),
        Product("c0000000-0000-0000-0000-000000000025", "REGAL-ECKE-MODERN", "Eckregal Modern", "Regale")
    ];

    private static WarehouseProductEntity Product(string id, string sku, string name, string category) => new()
    {
        ProductId = Guid.Parse(id),
        Sku = sku,
        Name = name,
        Category = category,
        AvailableQuantity = 30
    };
}
