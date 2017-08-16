# Bucketing of Accounts, based on market and payments

For Europe the realized and unrealized position are calculated by the EOD simulation within Endur which gives the realized and unrealized risk.
It then looks at the latest invoice status and payment information to work out the invoiced amount and the paid amount. The paid amount comes from outbound_pymt_reconcile.

The risk buckets are then calculated as:
- *MTM*: unrealized position
- *Accrual*: realized position – invoiced amount
- *ArAp*: invoiced amount – paid amount
