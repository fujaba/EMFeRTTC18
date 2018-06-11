FROM gradle
RUN rm -rf EMFeRTTC18
RUN git clone https://github.com/fujaba/EMFeRTTC18.git
WORKDIR EMFeRTTC18
RUN gradle benchmarkFull 