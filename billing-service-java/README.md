Start Strip local

docker run --rm -it -v ~/.config/stripe:/root/.config/stripe stripe/stripe-cli listen --forward-to host.docker.internal:8089/webhook


Pypal   :to get Webhook 

   1:  to get Authentication paypal:  https://developer.paypal.com/api/rest/authentication/


curl -v -X POST "https://api-m.sandbox.paypal.com/v1/oauth2/token" \
-u "CLIENT_ID:CLIENT_SECRET" \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=client_credentials" 

   2:  How to get Json : Doco : https://developer.paypal.com/docs/api/webhooks/v1/#verify-webhook-signature_post



Umgebung,PayPal Base-URL (Endpunkt),Sinn und Zweck
Sandbox (Dev),[https://api-m.sandbox.paypal.com](https://api-m.sandbox.paypal.com),Zum Testen mit Fake-Geld und Test-Usern
Production (Prod),[https://api-m.paypal.com](https://api-m.paypal.com),             Für echte Kunden und echtes Geld


Wie oft und in welchen Abständen schickt PayPal das Event neu?
PayPal überlässt das nicht dem Zufall, sondern nutzt einen intelligenten Algorithmus (exponentielles Backoff):

Maximale Versuche: PayPal versucht im Sandbox-Modus und im Live-Modus typischerweise, 

das Event über einen Zeitraum von bis zu 3 Tagen (72 Stunden) zuzustellen.

todo 
   . Events: SystemAlarm, SecurityAlarm, Duppel
