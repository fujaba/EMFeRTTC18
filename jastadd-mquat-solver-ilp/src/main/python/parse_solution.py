#!/usr/bin/python2
import collections
import sys

filename = sys.argv[1]
phase = 1
solution = collections.OrderedDict()

print 'parse_solution.py:'
with open(filename) as fdr:
    for line in fdr:
        if phase < 3:
            if line.startswith('Objective'):
                print line.strip()
            if line.startswith('---'):
                phase += 1
            continue
        if not line.strip():
            continue
        tokens = line.split()
        if len(tokens) == 6:
            try:
                index, name, star, activity, lb, rb = tokens
                solution[name] = int(activity)
            except ValueError:
                print 'Bad name+value tokens:', tokens
            finally:
                phase = 3
        elif phase == 3:
            if line.startswith('Integer'):
                break
            try:
                index, name = tokens
            except ValueError:
                print 'Bad name tokens:', tokens
            finally:
                phase = 4
        elif phase == 4:
            try:
                star, activity, lb, rb = tokens
                solution[name] = int(activity)
            except ValueError:
                print 'Bad value tokens:', tokens
            finally:
                phase = 3

print 'Read', len(solution), 'variables.'
all_zero = True
for key, value in solution.iteritems():
    if value == 1:
        print key, '=', value
        all_zero = False
if all_zero:
    print 'No variable has value 1'
