# Q4b
# Solution
# ########################################################################
month_list = []
trans_list = []
with open('transactions.txt','r') as in_file:
    for line in in_file:
        line = line.rstrip('\n')
        columns = line.split('\t')
        date = columns[0]
        month = date[3:]
        desc = columns[1]
        amount = columns[2]
        
        if month not in month_list:
            month_list.append(month)
            
        trans_list.append( (month, desc, amount) )

with open('transactions-output-2.txt','w') as out_file:
    for month in month_list:
        
        total = 0.0
        my_trans_list = []
        for curr_trans in trans_list:
            curr_month = curr_trans[0]
            curr_desc = curr_trans[1]
            curr_amount = curr_trans[2]
            
            if curr_month == month:
                total += float(curr_amount[1:])
                my_trans_list.append( (curr_desc, curr_amount) )
        
        out_file.write(month + ": total transaction amount is $" + str(total) + '\n')
        for curr_trans in my_trans_list:
            out_file.write('\t' + curr_trans[0] + '\t' + curr_trans[1] + '\n')
        out_file.write('\n')