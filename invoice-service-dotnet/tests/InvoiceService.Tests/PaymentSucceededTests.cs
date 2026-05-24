using InvoiceService.Domain;

namespace InvoiceService.Tests;

public sealed class PaymentSucceededTests
{
    [Fact]
    public void Create_NormalizesCurrencyAndKeepsPaymentData()
    {
        var payment = PaymentSucceeded.Create(
            Guid.NewGuid(),
            Guid.NewGuid(),
            " tx-123 ",
            42m,
            " eur ",
            "customer@example.com",
            NewCustomer(),
            [InvoiceLineItem.Create("SKU-LAMP", "Lamp", 1, 42m)]);

        Assert.Equal("EUR", payment.Currency);
        Assert.Equal("tx-123", payment.TransactionId);
        Assert.Single(payment.Items);
    }

    [Fact]
    public void Create_RejectsEmptyItems()
    {
        Assert.Throws<ArgumentException>(() => PaymentSucceeded.Create(
            Guid.NewGuid(),
            Guid.NewGuid(),
            "tx-123",
            42m,
            "EUR",
            "customer@example.com",
            NewCustomer(),
            []));
    }

    private static CustomerDetails NewCustomer()
    {
        return CustomerDetails.Create(
            "Max Mustermann",
            "customer@example.com",
            "Musterstrasse 12",
            "58507",
            "Luedenscheid",
            "Deutschland");
    }
}
