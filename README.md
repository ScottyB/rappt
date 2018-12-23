# RAPPT

RAPPT is a compiler and web application for generating Android apps using a Domain Specific Language.

## Repository Structure

* `rappt-compiler` - Source code for the RAPPT compiler written in Java. The compiler can be run as a Jar application and does not require the API or the web application.
* `rappt-api` - Source code for the web API written in Javascript
* `rappt-web` - Source code for the web application written in CoffeeScript
* `rappt-tool-guide.pdf` - A tool guide describing the RAPPT web application
* `grammar.g4` - The grammar for the App Modelling Language
* `criteria.md` - The criteria used as part of a user evaluation of RAPPT

## How it Works? (Roughly)

![](https://raw.github.com/scottyb/rappt/master/images/description.png)

1. Describe the app you want to build using the DSL.
2. A model of your app gets created by RAPPT.
3. A set of transformation rules get executed to create a new model.
4. Code gets generated from the new model!

## Example Output

App to copy on the left, generated output from RAPPT on the right!

![](https://raw.github.com/scottyb/rappt/master/images/screenshots.png)

## Links for more information

* Bootstrapping Mobile App Development: Short easy read [link](https://ts.data61.csiro.au/publications/nicta_full_text/8555.pdf)
* A Conceptual Model for Architecting Mobile Applications: Theoretical underpinnings for RAPPT [link](https://www.researchgate.net/publication/277017883_A_Conceptual_Model_for_Architecting_Mobile_Applications)
  - Core concepts that you need to know to build a mobile app
* A Multi-view Framework for Generating Mobile Apps: Web app for RAPPT [link](https://www.researchgate.net/publication/283716524_A_Multi-view_Framework_for_Generating_Mobile_Apps)
* Thesis: Full details, LONG read [link](https://researchbank.swinburne.edu.au/items/458ce762-3ba9-441c-a389-933640d828fb/1/)
