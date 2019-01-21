#!/bin/bash
pdflatex osi_slides.tex
bibtex osi_slides.aux
pdflatex osi_slides.tex
pdflatex osi_slides.tex
