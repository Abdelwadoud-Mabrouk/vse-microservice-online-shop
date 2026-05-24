namespace InvoiceService.Domain;

public sealed record CustomerDetails(
    string Name,
    string Email,
    string Street,
    string PostalCode,
    string City,
    string Country)
{
    public static CustomerDetails Create(
        string name,
        string email,
        string street,
        string postalCode,
        string city,
        string country)
    {
        return new CustomerDetails(
            Required(name, nameof(name)),
            Required(email, nameof(email)).ToLowerInvariant(),
            Required(street, nameof(street)),
            Required(postalCode, nameof(postalCode)),
            Required(city, nameof(city)),
            Required(country, nameof(country)));
    }

    private static string Required(string value, string name)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            throw new ArgumentException($"{name} is required.", name);
        }

        return value.Trim();
    }
}
