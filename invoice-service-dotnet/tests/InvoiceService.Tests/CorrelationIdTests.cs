using InvoiceService.Api;
using Microsoft.AspNetCore.Http;

namespace InvoiceService.Tests;

public sealed class CorrelationIdTests
{
    [Fact]
    public void GetCorrelationId_WhenBodyContainsCorrelationId_UsesBodyValue()
    {
        var generatedHeaderCorrelationId = Guid.Parse("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
        var eventCorrelationId = Guid.Parse("10000000-0000-4000-8000-000000000301");
        var context = new DefaultHttpContext();
        context.Items["CorrelationId"] = generatedHeaderCorrelationId;

        var result = context.GetCorrelationId(eventCorrelationId);

        Assert.Equal(eventCorrelationId, result);
        Assert.Equal(eventCorrelationId.ToString(), context.Response.Headers["X-Correlation-Id"]);
    }

    [Fact]
    public void GetCorrelationId_WhenBodyDoesNotContainCorrelationId_UsesMiddlewareValue()
    {
        var generatedHeaderCorrelationId = Guid.Parse("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
        var context = new DefaultHttpContext();
        context.Items["CorrelationId"] = generatedHeaderCorrelationId;

        var result = context.GetCorrelationId(Guid.Empty);

        Assert.Equal(generatedHeaderCorrelationId, result);
    }
}
