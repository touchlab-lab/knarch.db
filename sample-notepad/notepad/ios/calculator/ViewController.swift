//
//  ViewController.swift
//  calculator
//
//  Created by jetbrains on 01/12/2017.
//  Copyright © 2017 JetBrains. All rights reserved.
//

import UIKit
import KotlinArithmeticParser

class ViewController: UIViewController {
    let noteModel = KAPNoteModel()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.

    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    @IBAction func insertStuffAction(_ sender: Any) {
        noteModel.insertTestNotes()
        outputLabel.text = "Ran with memory dumps"
    }
    
    @IBAction func memoryAction(_ sender: Any) {
        noteModel.insertTestNotes()
        outputLabel.text = "Ran with memory dumps"
    }
    
    @IBOutlet weak var outputLabel: UILabel!
    @IBOutlet var partialResult: UILabel!



}


