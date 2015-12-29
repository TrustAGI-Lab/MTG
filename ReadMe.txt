MTG -- Source codes and Data used for 
	Shirui Pan, Jia Wu, Xingquan Zhu, Chengqi Zhang, and Philip Yu. Joint Structure Feature Exploration and Regularization for Multi-Task Graph Classification. TKDE, 2015.

Description: 
	This package includes two variants of MTG, i.e., MTG-l1 and MTG-l21. In general, MTG iteratively solves two subproblems: 
		(1) Multi-task learning (MTL) for vector data with logistic loss function
		(2) Most discriminative subgraph selection
	For the first subproblem, we employ MALSAR solver [1] to solve the multi-task problem. For the second subproblem, MTG  mploys a Top K subgraph miner in Java with upper bounds to prune the unpromising subgraph space.


Folders and Files:
	src/ : core scripts for MTG algorithm;
	MALSAR/ : a solver for solving the MTL problem;
	GMiner : Top-K discriminative subgrpah mining written in JAVA, it also provides source code for subgraph base graph classification, i.e., first mine a set of frequent subgraphs, and then employ SVMs for graph classification;
	data/ : NCI data used in the report
	mtg_result/ : results obtained from the demo

Demo:
	run demo_MTG.m for result

Other Reference
	1. J. Zhou, J. Chen and J. Ye. MALSAR: Multi-tAsk Learning via StructurAl Regularization. Arizona State University, 2012. http://www.public.asu.edu/~jye02/Software/MALSAR.


Tips:
If come across Out of Memory error, increase the Java Heap Space in Matlab:
    Preferences -> General -> Java Heap Space
    restart matlab
	 



