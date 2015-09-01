function [acc,auc,C_out,loss] = evaluatePer(X, Y, lfun, W_pred, C_pred)
    l = length(Y);

    if strcmp(lfun,'logistic')
         C_out = X * W_pred+C_pred;
         loss=sum(log(1+exp(-Y.*C_out)));
    else
        if strcmp(lfun, 'least')
            C_out = X * W_pred;   
            loss=1/2*sum((Y-C_out).*(Y-C_out))
        else
            disp('Error, not support....')
        end
    end
    
     acc = nnz(sign(C_out) == Y)/l;
    
    
    [auc, eer, curve] = rocscore (C_out,Y);
    % disp(['ACC:',num2str(acc),'(',num2str(nnz(sign(C_out) == Y)),'/',num2str(l),')    ','AUC:',num2str(auc)])
  %  disp(['AUC:',num2str(auc)])
    
       
end