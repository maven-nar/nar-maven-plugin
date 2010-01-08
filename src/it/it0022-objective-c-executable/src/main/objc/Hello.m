#import "Talker.h"

int main(void) {
  Talker *talker = [[Talker alloc] init];
  [talker say: "Hello World!"];
  [talker release];
}

