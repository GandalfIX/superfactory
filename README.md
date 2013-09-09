SuperFactory
============

What is SuperFactory?
SuperFactory is a possiblity to create objects without actually using their constructor or creating on Factory classes.

Why built SuperFactory?
While being at a clients side I tried to make an existing monolithic Java application testable. But as it was wired completely totally static I tried to convince the customer to introduce Spring or a similar framework.
But as these external frameworks were not yet approved by security and "would introduce too much complexity", I decided to build the first version of SuperFactory.
This allowed me to replace all of the constructor call within the business logic to allow individual components to be tested as now it is possible to "inject" mocks pretty everywhere.
