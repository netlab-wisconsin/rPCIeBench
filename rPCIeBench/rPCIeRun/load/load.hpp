#ifndef _LOAD_HPP_
#define _LOAD_HPP_

#include <ctype.h>
#include <unistd.h>
#include <QDMAController.h>
#include <chrono>
using namespace std;

#define DEFAULT_DATA_BYTE_LENGTH        1024
#define DEFAULT_COMMANDS                (256*1024)

#define H2C_ADDRESS_HIGH                100
#define H2C_ADDRESS_LOW                 101
#define H2C_DATA_BYTE_LENGTH            102
#define H2C_DATA_SOP                    103
#define H2C_DATA_EOP                    104
#define H2C_DATA_TOTAL_WORDS            105
#define H2C_DATA_TOTAL_COMMANDS         106
#define H2C_DATA_START                  110

#define C2H_ADDRESS_HIGH                120
#define C2H_ADDRESS_LOW                 121
#define C2H_DATA_BYTE_LENGTH            122
#define C2H_DATA_TOTAL_WORDS            123
#define C2H_DATA_TOTAL_COMMANDS         124
#define C2H_PFCH_TAG                    125
#define C2H_TAG_INDEX                   126
#define C2H_DATA_START                  130

#define H2C_ERROR_COUNT                 (512+100)
#define H2C_TIME_COUNT                  (512+101)
#define H2C_WORDS_COUNT                 (512+102)
#define H2C_COMMANDS_STATISTICS         (512+7)
#define H2C_COMMANDS_CHECK              (512+10)

#define C2H_COMMANDS_COUNT              (512+110)
#define C2H_TIME_COUNT                  (512+111)
#define C2H_WORDS_COUNT                 (512+112)
#define C2H_COMMANDS_STATISTICS         (512+2)
#define C2H_COMMANDS_CHECK              (512+6)

#define ENABLE_CTIME


bool startFpgaH2C(uint32_t* buffer, volatile uint32_t* axilReg);
void pauseFpgaH2C(volatile uint32_t* axilReg);
void resumeFpgaH2C(volatile uint32_t* axilReg);


bool startFpgaC2H(volatile uint32_t* buffer, volatile uint32_t* axilReg);
void pauseFpgaC2H(volatile uint32_t* axilReg);
void resumeFpgaC2H(volatile uint32_t* axilReg);

void axilBenchmarkInit(volatile uint32_t* axilReg);
uint64_t axilReadBenchmark(volatile uint32_t* axilReg);



#endif