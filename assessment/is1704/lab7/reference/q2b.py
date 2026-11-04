# Q2b: Universities 
# Solution
# ########################################################################

def get_average_num_students(filename):
    tot_num_students = 0
    with open(filename, 'r') as my_file:
        count = 0
        for line in my_file:
            line = line.rstrip('\n')
            columns = line.split('\t')
            num_students = int(columns[3])
            tot_num_students += num_students
            count += 1
    if count != 0:
        return tot_num_students / count
    return 0




