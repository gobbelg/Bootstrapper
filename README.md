  The Boostrapper class implements bootstrapping to calculate confidence
  intervals for AUC, sensitivity, specificity, negative predictive value,
  positive predictive value, and F1. Confidence intervals are based on the
  number of true positives, false positives, true negatives, and false
  negatives in the sample.
 
  The class is meant to be run from the main() method of this class. The value
  calculated is set within main() by setting the runType variables to one of
  the RunType enum values. By default, the calculations will run 10000
  iterations (as set by the static variable ITERATIONS) and calculate the 95%
  confidence interval (as set by the static variable CONFIDENCE_INTERVAL).
 
  Setting main() to run the RunType of BASE_CASE allows insertion of particular
  values of true positives, false positives, true negatives, and false
  negatives directly in code, and the metric run (e.g AUC, F1, etc) can be
  changed by setting the BASE_CASE_METRIC static variable to another function
  of type Function<Map<Outcome, Integer>, Double> that implements the 'apply'
  method. All the classes that currently do this are stored in the 'metrics'
  package of this application.
 
  The various MULTI_VALUE methods allow calculations based on multiple sets of
  values. The values should be stored in a tab-delimited file created so that
  the first line of the file contains headers indicating which columns contain
  the variable name, true positives (labeled as 'TP' without the quotes), false
  positives (labeled as 'FP'), true negatives (labeled as 'TN'), and false
  negatives (labeled as 'FN'). Each subsequent line should consist of the
  variable for which the confidence interval is being calculated, followed by
  the number of true positives, false positives, true negatives, and false
  negatives in the order specified by the header. The Examples folder in this
  project contains a file that can be used with the MULTI_VALUE RunTypes. The
  full path to the file should be set within the various cases identified by
  various RunTypes within the switch statement of main().
