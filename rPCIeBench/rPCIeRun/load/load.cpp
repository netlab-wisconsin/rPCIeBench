#include "load.hpp"
#include <QDMAController.h>
#include <unistd.h>
#include <ctype.h>
#include <ctime>

void pauseFpgaH2C(volatile uint32_t* axilReg) {
    axilReg[H2C_DATA_START] = 0;
}
void resumeFpgaH2C(volatile uint32_t* axilReg) {
    axilReg[H2C_DATA_START] = 1;
}

bool startFpgaH2C(uint32_t* buffer, volatile uint32_t* axilReg) {
    uint32_t addressHigh = (uint32_t)((unsigned long)(buffer) >> 32);
    uint32_t addressLow = (uint32_t)((unsigned long)(buffer));
    uint32_t dataLength = DEFAULT_DATA_BYTE_LENGTH;
    uint32_t sop = 1;
    uint32_t eop = 1;
    uint32_t totalCommands = DEFAULT_COMMANDS;
    uint32_t totalWords = dataLength / 64 * totalCommands;

    for (uint32_t i = 0; i < totalWords; i++) {
        for (uint32_t j = 0; j < 16; j++) {
            buffer[16 * i + j] = i;
        }
    }
    axilReg[H2C_ADDRESS_HIGH] = addressHigh;
    axilReg[H2C_ADDRESS_LOW] = addressLow;
    axilReg[H2C_DATA_BYTE_LENGTH] = dataLength;
    axilReg[H2C_DATA_SOP] = sop;
    axilReg[H2C_DATA_EOP] = eop;
    axilReg[H2C_DATA_TOTAL_COMMANDS] = totalCommands;
    axilReg[H2C_DATA_TOTAL_WORDS] = totalWords;

    sleep(1);
    cerr << "H2C Verification Start!" << endl;
    auto timeCountBegin = axilReg[H2C_TIME_COUNT];
    resumeFpgaH2C(axilReg);    // start
    sleep(2);
    pauseFpgaH2C(axilReg);
    sleep(2);               // end
    auto errorCount = axilReg[H2C_ERROR_COUNT];
    auto timeCountEnd = axilReg[H2C_TIME_COUNT];
    auto timeCount = timeCountEnd - timeCountBegin;
    cerr << "H2C Verification Complete!" << endl;
    cerr << "Number of Errors: " << errorCount << ", Number of Cycles: " << timeCount << endl;
    
    // verification
    auto commandsGenerated = axilReg[H2C_COMMANDS_STATISTICS];
    auto commandsEmitted = axilReg[H2C_COMMANDS_CHECK];
    fprintf(stderr, "io.h2c_cmd: %u fifo_h2c_cmd.io.out: %u\n", commandsGenerated, commandsEmitted);
    if (commandsGenerated == commandsGenerated && errorCount == 0) {
        fprintf(stderr, "H2C Check Passed! Bandwidth: %.2lfGBps\n", 1.0*dataLength*commandsEmitted/(1.0*timeCount*4/1000/1000/1000)/1024/1024/1024);
        return true;
    } else {
        fprintf(stderr, "H2C Check Failed! Benchmark Aborted!\n");
        return false;
    }

}


void pauseFpgaC2H(volatile uint32_t* axilReg) {
    axilReg[C2H_DATA_START] = 0;
}
void resumeFpgaC2H(volatile uint32_t* axilReg) {
    axilReg[C2H_DATA_START] = 1;
}
bool startFpgaC2H(volatile uint32_t* buffer, volatile uint32_t* axilReg) {
    uint32_t addressHigh = (uint32_t)((unsigned long)(buffer) >> 32);
    uint32_t addressLow = (uint32_t)((unsigned long)(buffer));
    uint32_t dataLength = DEFAULT_DATA_BYTE_LENGTH;
    uint32_t totalCommands = DEFAULT_COMMANDS;
    uint32_t totalWords = dataLength / 64 * totalCommands;

    for (uint32_t i = 0; i < totalWords; i++) {
        for (uint32_t j = 0; j < 16; j++) {
            buffer[16 * i + j] = 0;
        }
    }

    axilReg[C2H_ADDRESS_HIGH] = addressHigh;
    axilReg[C2H_ADDRESS_LOW] = addressLow;
    axilReg[C2H_DATA_BYTE_LENGTH] = dataLength;
    axilReg[C2H_DATA_TOTAL_COMMANDS] = totalCommands;
    axilReg[C2H_DATA_TOTAL_WORDS] = totalWords;
    writeConfig(0x1408/4, 0);
    auto tag = readConfig(0x140c/4);
    axilReg[C2H_PFCH_TAG] = tag;
    axilReg[C2H_TAG_INDEX] = 0;

    sleep(1);
    cerr << "C2H Verification Start!" << endl;
    auto timeCountBegin = axilReg[C2H_TIME_COUNT];
    resumeFpgaC2H(axilReg);       // start
    sleep(2);
    pauseFpgaC2H(axilReg);        // end
    sleep(2);
    auto timeCountEnd = axilReg[C2H_TIME_COUNT];
    auto wordsCount = axilReg[C2H_WORDS_COUNT];
    auto commandsCount = axilReg[C2H_COMMANDS_COUNT];
    auto timeCount = timeCountEnd - timeCountBegin;
    cerr << "C2H Verification Complete!" << endl;

    uint32_t errorCount = 0;
    for (uint32_t i = 0; i < totalWords; i++) {
        if (buffer[16 * i] != i) {
            errorCount++;
        }
    }
    cerr << "Number of Errors: " << errorCount << ", Number of Cycles: " << timeCount << ", Number of Words: "<< wordsCount << ", Number of Commands: " << commandsCount << endl;

    // verification
    auto commandsGenerated = axilReg[C2H_COMMANDS_STATISTICS];
    auto commandsEmitted = axilReg[C2H_COMMANDS_CHECK];
    if (commandsGenerated == commandsGenerated && errorCount == 0) {
        fprintf(stderr, "C2H Check Passed! Bandwidth: %.2lfGBps\n", 1.0*dataLength*commandsEmitted/(1.0*timeCount*4/1000/1000/1000)/1024/1024/1024);
        return true;
    } else {
        fprintf(stderr, "C2H Check Failed! Benchmark Aborted!\n");
        return false;
    }

}

void axilBenchmarkInit(volatile uint32_t* axilReg) {
    for (int i = 0; i < 256; i++) {
        axilReg[256 + i] = 512 + 256 + i;
        axilReg[512 + 256 + i] = 256 + i + 1;
    }
}

uint64_t axilReadBenchmark(volatile uint32_t* axilReg) {
    uint32_t index;
    #ifdef ENABLE_CTIME
        struct timespec start_timer, end_timer;
        clock_gettime(CLOCK_MONOTONIC, &start_timer);
    #else
        auto start = chrono::system_clock::now();
    #endif
    index = axilReg[256];
    for (int i = 0; i < 511; i++) {
        index = axilReg[index];
    }
    #ifdef ENABLE_CTIME
        clock_gettime(CLOCK_MONOTONIC, &end_timer);
        return ((end_timer.tv_sec-start_timer.tv_sec) * 1e9 + end_timer.tv_nsec-start_timer.tv_nsec);
    #else
        auto end = chrono::system_clock::now();
        auto duration_us = chrono::duration_cast<chrono::nanoseconds>(end - start);
        return duration_us.count();
    #endif
    
}
