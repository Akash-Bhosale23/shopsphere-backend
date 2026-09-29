A user story is one sentence: "As a ___, I want to ___, so that ___." Here's our starting list for the first version (MVP, the smallest useful version):

Authentication

As a user, I can register and log in.
As a user, I can view and update my profile.

Catalog

As an admin, I can create categories (Electronics, Books...).
As a seller, I can add, edit and delete my products.
As anyone, I can browse products with search, filter and pagination.

Cart and Orders

As a customer, I can add or remove products in my cart.
As a customer, I can place an order from my cart.
As a customer, I can see my order history and cancel an order.
As an admin, I can update an order's status.

An endpoint is a URL plus an HTTP method. The methods are: GET (read), POST (create), PUT (update), DELETE (remove). Here is a first draft (the /api/v1/ prefix means version 1, so we can ship a v2 later without breaking old apps):

POST   /api/v1/auth/register
POST   /api/v1/auth/login

GET    /api/v1/products
GET    /api/v1/products/{id}
POST   /api/v1/products          (seller)
PUT    /api/v1/products/{id}     (seller)
DELETE /api/v1/products/{id}     (seller)

GET    /api/v1/categories
POST   /api/v1/categories        (admin)

GET    /api/v1/cart
POST   /api/v1/cart/items
DELETE /api/v1/cart/items/{id}

POST   /api/v1/orders
GET    /api/v1/orders
PUT    /api/v1/orders/{id}/status (admin)