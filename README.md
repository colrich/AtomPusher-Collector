# AtomPusher-Collector
### The RESTful backend component of the AtomPusher system

* Get the code
  - Clone this repository: https://github.com/colrich/AtomPusher-Collector.git
  - Edit manifest.yml and give the application a unique name
  - The hostname of this app is the host you must configure the AWS Lambda function to call. Instructions for that are in the AtomPusher-Lambda repository's README.
  - Run 'mvn package'
* Push the app
  - Run 'cf push'
  - Monitor the logs to ensure that entries are flowing through the system
  - Feed data will become available over time at https://<yourhost>/f/<feedtag>/<index>
