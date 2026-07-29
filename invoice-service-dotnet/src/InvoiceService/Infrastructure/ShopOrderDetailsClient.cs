using System.Net.Http.Json;
using System.Text.Json;
using InvoiceService.Application;
using InvoiceService.Domain;

namespace InvoiceService.Infrastructure;

public sealed class ShopOrderDetailsClient(
    HttpClient httpClient,
    ILogger<ShopOrderDetailsClient> logger) : IOrderDetailsProvider
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        PropertyNameCaseInsensitive = true
    };

    public async Task<OrderInvoiceDetails> GetAsync(Guid orderId, CancellationToken cancellationToken)
    {
        logger.LogInformation(
            "Loading invoice details from shop-service for order {OrderId}",
            orderId);

        var response = await httpClient.GetFromJsonAsync<ShopOrderResponse>(
            $"api/v1/orders/{orderId}",
            JsonOptions,
            cancellationToken)
            ?? throw new InvalidOperationException($"Shop order {orderId} returned an empty response.");

        if (response.OrderId != orderId)
        {
            throw new InvalidOperationException(
                $"Shop returned order {response.OrderId} for requested order {orderId}.");
        }

        var customer = response.Customer
            ?? throw new InvalidOperationException($"Shop order {orderId} is missing customer data.");
        var shippingAddress = response.ShippingAddress
            ?? throw new InvalidOperationException($"Shop order {orderId} is missing shipping data.");
        var items = response.Items
            ?? throw new InvalidOperationException($"Shop order {orderId} is missing order items.");

        var customerDetails = CustomerDetails.Create(
            JoinRequired(customer.FirstName, customer.LastName),
            customer.Email,
            JoinRequired(shippingAddress.Street, shippingAddress.HouseNumber),
            shippingAddress.PostalCode,
            shippingAddress.City,
            shippingAddress.Country);

        var invoiceItems = items
            .Select(item => InvoiceLineItem.Create(
                item.ProductId.ToString(),
                item.ProductName,
                item.Quantity,
                item.PricePerUnit))
            .ToArray();

        return new OrderInvoiceDetails(customer.Email, customerDetails, invoiceItems);
    }

    private static string JoinRequired(params string?[] parts)
    {
        return string.Join(' ', parts.Where(part => !string.IsNullOrWhiteSpace(part)).Select(part => part!.Trim()));
    }

    private sealed record ShopOrderResponse(
        Guid OrderId,
        ShopCustomer? Customer,
        ShopShippingAddress? ShippingAddress,
        IReadOnlyList<ShopOrderItem>? Items);

    private sealed record ShopCustomer(
        string FirstName,
        string LastName,
        string Email,
        string? Phone);

    private sealed record ShopShippingAddress(
        string Street,
        string HouseNumber,
        string PostalCode,
        string City,
        string Country);

    private sealed record ShopOrderItem(
        Guid ProductId,
        string ProductName,
        int Quantity,
        decimal PricePerUnit);
}
