FROM nginx:1.23

COPY frontend.nginx.conf /etc/nginx/nginx.conf

EXPOSE 8080