FROM nginx:1.23

COPY backend.nginx.conf /etc/nginx/nginx.conf

EXPOSE 8080