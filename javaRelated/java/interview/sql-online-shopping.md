# SQL Test - Online Shopping System 

*WIP*

## The Problem
Design a relational database to store below information for an online shopping system: (10min)

The system have a selection of items with name, price, quantity etc 
User can add item to their shopping basket.
User can check out to complete an order.

## Notes
This is a well known problem (eg Amazon). Choice of what to store in db has a few treadeoffs.
And the idea of the basket can introduce a few difficulties, as it needs to be kept for a session, which may/not be kept in db.
Qty changes in basket...
Discounting also causes issues..

## Tables

Order ---<- Item ->--- Basket

Item: name, price

## SQL Queries

### Get each of those orders’ total quantities
select orderNum, sum(qty)
from Order
  inner join Item on
group by orderNum

### Get all of those orders with total quantity > 7000

where qty > 7000

### Get any orders with more than 5 of item A 

select orderNum
from Item
  inne rjoin Order
group by orderNum
having count(itemNum) > 5
