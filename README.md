# SuperFactory

## What is SuperFactory?
SuperFactory is a possiblity to create objects without actually using their constructor directly in the code.

## Why built SuperFactory?
While being at a clients side I tried to make an existing monolithic Java application testable. 
But as it was wired completely totally static I tried to convince the customer to introduce Spring or a similar framework.
But as these external frameworks were not yet approved by security and "would introduce too much complexity", I decided to build the first version of SuperFactory, 
in order to have a self written piece of code which can help
This allowed me to replace all of the constructor call within the business logic to allow individual components to be tested, 
as now it is possible to "inject" mocks pretty everywhere where an object is created.

## Should you use SuperFactory?
If you have the same problem like me you can just import the two class files and everything is fine.
If you have the possibility to use Spring, Guice or any other "big" dependency injection framework, use them.

## Can you just copy the source code into your own namespace?
Sure! That is the reason why I try to keep SuperFactory very small with regards of number of classes.
So it is simple to just drop the actual code into any other code base.