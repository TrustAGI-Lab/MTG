%Experiments for Multi-task graph classification
clear;
clc;
clear java;


%In the TKDE paper, nine files are used.
%files={'1','33','41','47','81','83','109','123','145'};

%for a demo, we go with 5 files
files={'1','33','41','47','81'};



noTrain = 50;
regularizationStr='l21'; % set to l1 or l21, indicating MTG-l1 or MTG-l21
gamma = 0.02; 
noRun = 1; %run noRun times to get the average result, typical for 10 in
%     the TKDE paper, for demo, we set to 1

% Run MTG-l1 algorithm, results are written in the folder 'mtg-result'
MTG(files,noTrain,regularizationStr,gamma,noRun);


