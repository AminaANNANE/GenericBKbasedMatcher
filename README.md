# GenericBKbasedMatcher

This project is a generic background knowledge based ontology matching. It has been implemented this framework during my thesis that aimed to enhance ontology matching using external knowledge resources. It served for the evaluation of our approach. Please see [1] for more detail.
For the reuse purpose, our framework uses any direct matcher as a black box. In addition, it offers several parameters that allows to have various configurations according to the user needs.

We used this framework to participate in the OAEI 2017.5 compaign, with YAM++[2] as a direct matcher and two biomedical ontologies as background knowledge DOID and UBERON. The results may be found here https://goo.gl/A496ug

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites
The direct matcher instanciated in the Parameters class has to implement a static function align which takes as input the URL of ontologies to align, and returns the URL of the generated alignment:
        URL alignment=align (URL ontology1, URL ontology2) 
 Please, before running the code be sure that all libraries related to the used direct matcher are referenced.

The framework offers two derivation strategies (Neo4j and specific_algo). When using the Neo4j strategy, the Neo4j graph database should be installed and variables driver and session in the Parameters class should be initialized.
DataSets for ML based selection

### Installing





[1] https://www.sciencedirect.com/science/article/pii/S1570826818300179?via%3Dihub.

[2] http://www.websemanticsjournal.org/index.php/ps/article/view/483.
