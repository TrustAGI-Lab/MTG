%% Joint Structure Feature Exploration and Regularization for Multi-task Graph Classification
% by Shirui Pan, Jia Wu, Xingquan Zhu, Chengqi Zhang, and Philip S. Yu
% accepted by TKDE-2015.
%
% Example of runing NCI graph classification, codes written by matlab and
% Java, i.e., subgraph selection by Java and objective function solved by
% matlab. Matlab calls and stores Java objects.
% 
%
%   files: NCI graph file Id
%   noTrain: no of training graphs in each task
%   regularizationStr: l1 or l21, determining using MTG-l1 or MTG-l21 graph
%                      classification method
%   gamma: regularization parameter
%    noRun: run noRun times to get the average result, typical for 10 in
%     the TKDE paper
%
%
function MTG(files,noTrain,regularizationStr,gamma,noRun)

%% Set the path for MTG-optimization
addpath('../MALSAR/functions/Lasso/'); % load function
addpath('../MALSAR/utils/'); % load utilities
addpath('../MALSAR/functions/joint_feature_learning/');
javasetting

tstart=tic;

lfun='logistic'; %logistic or least

opts.rho_L2=0;%0.025;


no_stumps = 15; %default 15 or 10
maxPad=8; %DEFALT 8, OR 10 IS OK
maxInte = 15; %default 15
%% setting 
conv_epsilon=0.005; %0.005 for NCI,0.001 for protein;




%% Java Reading Graphs, Split into Training and Testing set
import util.*;
import moss.*;

%noRun=1;
allacc=zeros(noRun,length(files));
allauc=zeros(noRun, length(files));
for se=1:noRun
    
    alltrain_G=java.util.ArrayList;
    alltrain_Y=[];
    GraphUtils.setGraphType(DataConvert.Chemical);
    %GraphUtils.setGraphType(DataConvert.DBLP);
    
    iter = 1;
    for i = 1:length(files)
        disp(['Reading task',num2str(i),'.....'])
        graphs=GraphUtils.readGraphs(['../data/',files{i},'-balance.sdf'], 0);% NCI
       % graphs=GraphUtils.readGraphs(['./data/protein/',files{i},'.nel'], 0);%protein
       % graphs=GraphUtils.readGraphs(['./data/ptc_data/PTC_pn_',files{i},'_2part.smi'], 0);

        %split into training and test
        split = TrainTestSplit;
        split.setSeed(se);
      %  split.split(graphs,noTrain); %for protein, PTC

        split.splitbyNo(graphs,noTrain); % for NCI

        train_G{i} = split.trainG;
        test_G{i} = split.testG;
        train_Y{i}=split.trainY;
        test_Y{i}=split.testY;
        


        no_p = nnz(train_Y{i}==1);
        no_n = nnz(train_Y{i}==-1);
        disp(['pos:',num2str(no_p),'  neg:',num2str(no_n)])
        l(i) = no_p+no_n;
                 

        alltrain_G.addAll(train_G{i});
        alltrain_Y=[alltrain_Y; train_Y{i}];
        VX{i}=[];
        test_V{i}=[];

        %initialize
        u1=ones(no_p,1)*0.5/no_p;
        u2=ones(no_n,1)*0.5/no_n;
        %u=[u1;u2];
        u{i}=ones(l(i),1)/l(i);

        GraphUtils.setWeights(train_G{i},u{i});
        %u=[no_n/no_p*ones(no_p,1);ones(no_n,1)];

    end

    noT = length(files); %number of tasks
    totalnoTrain=alltrain_G.size;  %total number of training
    disp(['Total number of training graphs:',num2str(totalnoTrain)])

    selectG=java.util.ArrayList; 
    allstumps = java.util.ArrayList;




    %% Initialize weight


    while true
        disp(['Running gspan....,iterator:',num2str(iter)])

       %
        gminer = GraphBoostMinerGraphsMtask;
        gminer.mtaskGamma = gamma;
        gminer.mtaskVarepsilon = conv_epsilon;
        gminer.regularizationStr =regularizationStr;
    
        supp=0; %do not set a support for MTG
        
        stms = gminer.getMaxGainDecisionStumps(alltrain_G,selectG, supp, 0, no_stumps, maxPad, 0, l);

        %gminer = GraphBoostMinerGraphs;
      %  stms = gminer.getMaxGainDecisionStumps(alltrain_G,selectG, 0, gamma, no_stumps, maxPad, 0);

        rst=stms.size;
        disp(['gspan returning ',num2str(rst), ' graph stumps!current iterator:',num2str(iter)])

        if rst >0
           % oval = stms.descendingIterator.next.getGain;
           beststm = stms.descendingIterator.next;
           oval = beststm.getmTaskNormScore;

           ovalScore = beststm.getGain;
            disp(['max mean gain:(',num2str(ovalScore),' / ', num2str(oval),' )'])

                %oval = (u.*Y)'*h{1}.h(X);
            disp(['   optimality norm: ', num2str(oval), ' <= ', num2str(gamma ), ' + ', ...
                num2str(conv_epsilon), ' ?']);
        end


        % Stopping condition: either no hypotheses are there anymore or best
        % hypothesis gain is too small.
        %if rst == 0 || oval/totalnoTrain <= (gamma+ conv_epsilon)
        if rst == 0 %|| oval <= (gamma+ conv_epsilon) %iF we used another condition, the result will be a little better
            disp(['MTG optimality reached after ', num2str(iter), ' iterations.']);

            if rst == 0
                disp(['   (no hypotheses left)']);
            else
                disp(['   (no improvements left)']);
            end
          %  m = m - 1;
            break;
        end


        stms_array = GraphUtils.getStumpsFromTreeset(stms);
        allstumps.addAll(stms_array);
        selectG=GraphUtils.getGraphFromStumps(allstumps);
        disp(['num of already selected graphs:',num2str(selectG.size)])

        for i = 1 : length(files)

            Htrain=GraphUtils.graph2Vector(train_G{i},stms_array);
            VX{i} = [VX{i}, Htrain];


            Htest=GraphUtils.graph2Vector(test_G{i},stms_array);
            test_V{i}=[test_V{i},Htest];

            % Update combined hypothesis and restricted master problem

        end



        %
        % training and prediction using logistic loss
        if strcmp(regularizationStr,'l21') % MTG-l21
            %[W_pred, C_pred]= Logistic_Lasso(VX, train_Y, gamma,opts);
            [W_pred, C_pred,funVal]= Logistic_L21(VX, train_Y, gamma,opts);

            %compute the norm values;
            [nova,not]=size(W_pred);
            for i=1:nova
                nvs(i)=norm(W_pred(i,:),2);          
            end
            nv=norm(nvs,1);

        else % MTG-l1
            [W_pred, C_pred,funVal]= Logistic_Lasso(VX, train_Y, gamma,opts);

            nv=norm(W_pred,1);

        end

        for i = 1 : length(files)
            [acc(i),auc(i),C_out{i},lsv(i)] = evaluatePer(VX{i}, train_Y{i}, lfun, W_pred(:,i), C_pred(i));
            disp([files{i},':  ',num2str(acc(i)),'  ',num2str(auc(i))])



            if strcmp(lfun,'logistic')
              % nu{i}= -1./(exp(train_Y{i}.*C_out{i}) + 1);
               nu{i}= -1./(l(i)*(exp(train_Y{i}.*C_out{i}) + 1));
            else
                if strcmp(lfun, 'least')% least square
                    nu{i}=C_out{i} - train_Y{i};
                else
                    disp('Error, not support!')
                end
            end

          u{i}=nu{i};



             % u = u/sum(u);
          GraphUtils.setWeights(train_G{i},u{i}); 
          lsv(i) = lsv(i)/length(train_Y{i});

        end



        disp(['Average on Training:',num2str(mean(acc)),' / ',num2str(mean(auc))])
        objV(iter) = sum(lsv)+gamma*nv;
       % disp(num2str(funVal))
        disp(['FunVal:',num2str(funVal(length(funVal)))])
        disp(['Loss:',num2str(sum(lsv)),'   Regularization:',num2str(nv),'  Obj Val:',num2str(objV(iter)),]);


        if iter >= maxInte
            disp('end');
            break;
        end


        iter = iter + 1;
    end
    ttime=toc(tstart);
    
    m = allstumps.size;
    selectedIndex = abs(sum(W_pred,2)) > 1e-5;
    sparsity= nnz(abs(W_pred) < 1e-5)/(m*noT);
    disp(['active stumps:',num2str(nnz(selectedIndex)),',total:',num2str(m),'x',num2str(noT),'=',num2str(m*noT),',sparsity:',num2str(sparsity)])


    

   
    disp('Testing......')
    for i = 1 : length(files)

        [acc(i),auc(i),C_out{i}] = evaluatePer(test_V{i}, test_Y{i}, lfun, W_pred(:,i), C_pred(i));
         disp([files{i},':  ',num2str(acc(i)),'  ',num2str(auc(i))])
    % disp(['Final testing:',num2str(acc),' (',num2str(nnz(sign(C_out) == test_Y)),'/',num2str(length(test_Y)),')'])
    end
    allauc(se,:)=auc;
    allacc(se,:)=acc;
    disp(['Average on Testing:',num2str(mean(acc)),' / ',num2str(mean(auc))])
    disp(num2str(objV))
    
    
end %end for noRun 

meanAcc=mean(mean(allacc)),
meanAuc=mean(mean(allauc)),



para=[no_stumps,objV(length(objV)),iter,m,meanAcc,meanAuc,ttime];

paraGamma=[gamma,m,sparsity,meanAcc,meanAuc];

disp(num2str(paraGamma))
save(['../mtg_result/mtg',num2str(noTrain),'_',regularizationStr],'acc','auc','objV','para','meanAuc','meanAcc')



 disp(['Running time:',num2str(ttime)]);
end
