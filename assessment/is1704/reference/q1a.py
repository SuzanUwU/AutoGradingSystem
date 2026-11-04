# Name:
# Email ID:

def compute_parking(duration):
    # Replace the code below with your implementation.
    if duration <= 10:
        return 0.0
    elif duration <= 60:
        return 5.0
    else:
        return 5.0 + (duration - 60) * 0.1