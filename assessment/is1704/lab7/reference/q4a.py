## Q4a
# Solution
# ########################################################################

with open('transactions.txt','r') as in_file:
    with open('transactions-output-1.txt','w') as out_file:
        for line in in_file:
            line = line.rstrip('\n')
            columns = line.split('\t')
            price_dollar = columns[2]
            price = float(price_dollar[1:])
            if price >=30:
                out_file.write(line + '\n')