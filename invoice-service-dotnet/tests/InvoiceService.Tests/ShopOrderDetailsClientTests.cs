using System.Net;
using System.Text;
using InvoiceService.Infrastructure;
using Microsoft.Extensions.Logging.Abstractions;

namespace InvoiceService.Tests;

public sealed class ShopOrderDetailsClientTests
{
    [Fact]
    public async Task GetAsync_MapsShopOrderToInvoiceDetails()
    {
        var orderId = Guid.Parse("11111111-1111-4111-8111-111111111111");
        var productId = Guid.Parse("22222222-2222-4222-8222-222222222222");
        var handler = new StubHttpMessageHandler(
            $$"""
            {
              "orderId": "{{orderId}}",
              "customer": {
                "firstName": "Max",
                "lastName": "Mustermann",
                "email": "max@example.com",
                "phone": "012345"
              },
              "shippingAddress": {
                "street": "Musterstraße",
                "houseNumber": "12",
                "postalCode": "58507",
                "city": "Lüdenscheid",
                "country": "Deutschland"
              },
              "items": [
                {
                  "productId": "{{productId}}",
                  "productName": "Samt Sofa",
                  "quantity": 2,
                  "pricePerUnit": 499.50
                }
              ]
            }
            """);
        using var httpClient = new HttpClient(handler)
        {
            BaseAddress = new Uri("http://shop-service:8087/")
        };
        var client = new ShopOrderDetailsClient(
            httpClient,
            NullLogger<ShopOrderDetailsClient>.Instance);

        var result = await client.GetAsync(orderId, CancellationToken.None);

        Assert.Equal("max@example.com", result.CustomerEmail);
        Assert.Equal("Max Mustermann", result.Customer.Name);
        Assert.Equal("Musterstraße 12", result.Customer.Street);
        var item = Assert.Single(result.Items);
        Assert.Equal(productId.ToString(), item.Sku);
        Assert.Equal(2, item.Quantity);
        Assert.Equal(499.50m, item.UnitPrice);
        Assert.Equal($"http://shop-service:8087/api/v1/orders/{orderId}", handler.RequestUri?.ToString());
    }

    private sealed class StubHttpMessageHandler(string responseBody) : HttpMessageHandler
    {
        public Uri? RequestUri { get; private set; }

        protected override Task<HttpResponseMessage> SendAsync(
            HttpRequestMessage request,
            CancellationToken cancellationToken)
        {
            RequestUri = request.RequestUri;
            return Task.FromResult(new HttpResponseMessage(HttpStatusCode.OK)
            {
                Content = new StringContent(responseBody, Encoding.UTF8, "application/json")
            });
        }
    }
}
