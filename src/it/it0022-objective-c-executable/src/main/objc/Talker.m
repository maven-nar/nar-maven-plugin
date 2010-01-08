#import "Talker.h"

@implementation Talker

- (void) say: (char*) phrase {
  printf("%s\n", phrase);
}

@end

