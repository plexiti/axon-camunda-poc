## Proof of concept to integrate Axon's "Sagas" with Camunda's "Processes"

This is a simple POC to demonstrate the "style" of how I currently think that "the best 
of two worlds" could be combined to form a true power couple...

- Define a complex process with Camunda and leverage it's flow language expressiveness,
visualisation capabilities and production/live monitoring of state

- Define a corresponding Axon saga to manage the message handling and the whole data 
handling needed (such as constructing commands on the basis of current saga state)

Try out yourself the following example by starting : ./gradlew bootRun

<img src="https://raw.githubusercontent.com/plexiti/axon-camunda-poc/master/src/main/resources/com/plexiti/horizon/model/PaymentSaga.svg?sanitize=true"></img>

#### Successfully retrieve a payment with Martin's working credit card

- POST /accounts?name=martin
- POST /payments?account=martin&amount=25
- Note in the logs what happens in the saga flow. The payment is received.

#### Fail to retrieve a payment with Kermit's failing credit card

- POST /accounts?name=kermit
- POST /payments?account=kermit&amount=25
- Note in the logs what happens in the saga flow. Charging the credit card fails.
- Wait for a minute to see the credit card update reminders and the fail after a minute 

(Note that the timers trigger after approx 10 seconds instead of two days and approx 1 minute instead of two weeks!)

#### Give Kermit some credit and successfuly retrieve a payment without touching his card

- POST /accounts/kermit/credit?amount=30
- POST /payments?account=kermit&amount=25
- Note in the logs what happens in the saga flow. The "payment" is fully covered by the positive account balance.

#### Now with the remaining EUR 5 Kermit buys too much, again

- POST /payments?account=kermit&amount=25
- Note in the logs that the balance of 5 is retrieved and partially covering the payment
- For the remaining 20, the credit card still fails.
- Wait for a minute to see the credit card update reminders and the fail after a minute 
- Note in the logs that the balance of 5 is restored again

#### Now Kermit tries again. This time he updates his credit card details in due time<1

- POST /payments?account=kermit&amount=25
- Note in the logs that the balance of 5 is retrieved and partially covering the payment
- However, for the remaining 20, his card still fails...
- POST /accounts/kermit/updateCard (Within the minute until the timeout!)
- Now the remaining 20 are charged and the payment succeeds

(Note that for the showcase we just send a trigger without real credit card detail data)

#### Saga Operations Monitoring

- Now browse to http://localhost:8080 and log in with User: kermit / Pass: kermit
- Click on "Cockpit" to move into Camunda's operations monitoring.
- Browse to the running process instances and click on the "Payment" 
- You see the saga flow definition above
- Play with Kermit's failing credit card to see "live" that the running instances wait a minute for the credit card update

#### Proof of concept implementation to integrate Axon's Sagas with Camunda Processes

This is not a robust integration implementation, but a proof of concept to demonstrate
that the two tools can easily be integrated and to demonstrate a certain "mental model"
of how to do that. Basically my goal was to define the "pathes" in the visual Camunda model,
but the whole "data handling" necessary to create and issue commands etc in the saga.
Camunda's process engine flow is fully decoupled in the sense that the integration is 
itself leveraging Axon messages. The poc implementation is still naive in some ways, e.g.
one would have to address the need for a fully asynchronous query response handling etc.
I guess I just need a few hints from Axon to do that.

- This is how the "flow" enabled PaymentSaga currently looks like: 

https://github.com/plexiti/axon-camunda-poc/blob/master/src/main/kotlin/com/plexiti/horizon/model/PaymentSaga.kt

- This is the basic code for the Axon integration with flow engines - independent of Camunda's

https://github.com/plexiti/axon-camunda-poc/blob/master/src/main/kotlin/com/plexiti/integration/FlowsIntegration.kt

- This is the Camunda specific implementation of the integration:

https://github.com/plexiti/axon-camunda-poc/blob/master/src/main/kotlin/com/plexiti/integration/CamundaFlowsIntegration.kt

Enjoy! And don't forget to give me feedback: martin.schimak@plexiti.com

Martin.

PS: Did I mention that I see huge potential here? But just my five cents. :smile:

PS2: For the Camunda guys. When you open the model in Camunda Modeler, you will see that I'm leveraging element templates
to easily define the command, event and query message names.