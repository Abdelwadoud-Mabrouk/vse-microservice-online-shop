FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY src/AuditService/AuditService.csproj src/AuditService/
RUN dotnet restore src/AuditService/AuditService.csproj

COPY src/AuditService/ src/AuditService/
RUN dotnet publish src/AuditService/AuditService.csproj -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app
COPY --from=build /app/publish .
ENV ASPNETCORE_URLS=http://+:8080
EXPOSE 8080
ENTRYPOINT ["dotnet", "AuditService.dll"]
