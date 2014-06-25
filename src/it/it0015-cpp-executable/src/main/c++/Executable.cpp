/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
#include <stdio.h>
#include <example.h>

extern "C"
int main(int argc, char *argv[]) {
    Circle* c = new Circle(10);
    Square* s = new Square(10);

    if (Shape::nshapes != 2) return 1;

    c->x = 20;
    c->y = 30;

    Shape* shape = s;
    shape->x = -10;
    shape->y = 5;

    if (c->x != 20.0) return 2;
    if (c->y != 30.0) return 3;
    if (s->x != -10.0) return 4;
    if (s->y != 5.0) return 5;

    Shape* shapes[] = { c, s };

    if (shapes[0]->area() - 314.1592653589793 > 0.0001) return 6;
    if (shapes[0]->perimeter() - 62.83185307179586 > 0.0001) return 7;
    if (shapes[1]->area() - 100.0 > 0.0001) return 8;
    if (shapes[1]->perimeter() - 40.0 > 0.0001) return 9;

    delete c;
    delete s;

    if (Shape::nshapes != 0) return 10;

    return 0;
}


