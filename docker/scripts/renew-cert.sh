#!/bin/bash
docker-compose -f docker-compose-ssl.yml exec certbot certbot renew --quiet
docker-compose -f docker-compose-ssl.yml exec aiminder-client nginx -s reload