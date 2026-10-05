"""multimetric paleidiklis. `python -m multimetric` Windows+Git Bash aplinkoje užstringa
(multiprocessing spawn), todėl kviečiam main() tiesiogiai iš apsaugoto __main__ bloko."""
import sys
from multimetric.__main__ import main

if __name__ == "__main__":
    main()
