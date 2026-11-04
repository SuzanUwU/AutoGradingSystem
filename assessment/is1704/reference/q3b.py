# Name:
# Email ID:

from q3a import is_partially_compliant

def have_one_common_lang(st1_lang_list, st2_lang_list):
    have_one_common_lang = False
    for lang in st1_lang_list:
        if lang in st2_lang_list:
            have_one_common_lang = True  
    return have_one_common_lang
    
def have_less_than_3_common_skills(st1_skills_list, st2_skills_list):
    cnt = 0
    for skill in st1_skills_list:
        if skill in st2_skills_list:
            cnt += 1      
    if cnt > 2:
        # student1 and student2 have practiced more than 2 skills in common 
        # during their last respective internship
        return False
    return True

def satisfy_criteria(st1, st2):
    # student1 and student2 must speak at least one common language
    st1_lang_list = st1[3]
    st2_lang_list = st2[3]
    if not have_one_common_lang(st1_lang_list, st2_lang_list):
        return False
    # st1 and st2 must have practiced at most the same 2 skills during their last internship respectively
    st1_skills_list = st1[4]
    st2_skills_list = st2[4]
    if not have_less_than_3_common_skills(st1_skills_list, st2_skills_list):
        return False
    # student1 and student2 satisfy all the criteria
    return True

def is_fully_compliant(table):
    # if the table is not partially compliant return False
    if not is_partially_compliant(table):
        return False
    # let's check the 2 other criteria on common spoken language and common skills practiced
    for i in range(len(table)-1):
        prev_st = table[i]
        next_st = table[i+1]
        if not satisfy_criteria(prev_st, next_st):
            return False
    # check student A and student D satisfy the criteria also
    st1 = table[0]
    st3 = table[3]
    if not satisfy_criteria(st1,st3):
        return False
    return True
        