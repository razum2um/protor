# Protor

An application to handle incoming mails containing receipt JSONs from Russian tax
service.

[![Build Status][BS img]][Build Status]

Could also serve as example how to organize code with integrant
and how to deal with SMTP, integrant, duratom and some other stuff in Clojure.

## Name

Just [generated](https://mrsharpoblunto.github.io/foswig.js)

## WTF

Yes, our government knows everything what has been sold
and paid using credit/debit cards within minutes.
On the one side this prohibits tax evasion.

But in order to get this receipt we should register with a phone,
and every phone number in Russia is connected directly to a passport id.
So if we want to keep budget using this data, they know everything.

## Alternatives

[A lot](https://github.com/search?q=proverkacheka.nalog.ru%3A9999&type=Code)
including QR scanner, chatbots and hitting government api from server, but they already did
not a bad mobile app for us (for granted, lol),
and we can send JSON with receipt from there, so enough for me.

[Build Status]: https://travis-ci.com/razum2um/protor
[BS img]: https://travis-ci.com/razum2um/protor.png
