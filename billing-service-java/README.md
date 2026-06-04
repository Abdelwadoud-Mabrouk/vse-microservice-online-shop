Start Strip local

docker run --rm -it -v ~/.config/stripe:/root/.config/stripe stripe/stripe-cli listen --forward-to host.docker.internal:8089/webhook
