# Database Simple Order Query Problem

We have a schema with :

CUSTOMER
- id
- name
- address

ORDER
- id
- orderDate
- customerId (FK)

ITEM
- id
- description
- orderId (FK)

How to query all the items in a given Customer orderId
select name, odate, i.*
from customer c
	inner join order o on c.id = o.customerId
	inner join item i on o.id = i.orderId
where cust.name = ‘PAUL’

How do I query all the orders from a given date onwards?
select name, odate, i.*
from customer c
	inner join order o on c.id = o.id
where c.orderDate > '2022-10-01'


