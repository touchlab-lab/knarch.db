//
//  knarchtestrunTests.swift
//  knarchtestrunTests
//
//  Created by Kevin Galligan on 6/17/18.
//  Copyright © 2018 Kevin Galligan. All rights reserved.
//

import XCTest
import knarchtest
@testable import knarchtestrun

class knarchtestrunTests: XCTestCase {
    
    override func setUp() {
        super.setUp()
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }
    
    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
        super.tearDown()
    }
    
    func testKnarch() {
        XCTAssertEqual(KnarchtestTestHarness().testTest(), 0)
    }
    
    func testPerformanceExample() {
        // This is an example of a performance test case.
        self.measure {
            // Put the code you want to measure the time of here.
        }
    }
    
}
